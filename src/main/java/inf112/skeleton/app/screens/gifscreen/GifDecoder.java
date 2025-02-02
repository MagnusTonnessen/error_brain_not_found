package inf112.skeleton.app.screens.gifscreen;

/* Copyright by Johannes Borchardt */
/* LibGdx conversion 2014 by Anton Persson */
/* Released under Apache 2.0 */
/* https://code.google.com/p/animated-gifs-in-android/ */

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import java.io.InputStream;
import java.util.Vector;

public class GifDecoder {
    /**
     * File read status: No errors.
     */
    public static final int STATUS_OK = 0;
    /**
     * File read status: Error decoding file (may be partially decoded)
     */
    public static final int STATUS_FORMAT_ERROR = 1;
    /**
     * File read status: Unable to open source.
     */
    public static final int STATUS_OPEN_ERROR = 2;
    /** max decoder pixel stack size */
    protected static final int MAX_STACK_SIZE = 4096;
    protected InputStream in;
    protected int status;
    protected int width; // full image width
    protected int height; // full image height
    protected boolean gctFlag; // global color table used
    protected int gctSize; // size of global color table
    protected int loopCount = 1; // iterations; 0 = repeat forever
    protected int[] gct; // global color table
    protected int[] lct; // local color table
    protected int[] act; // active color table
    protected int bgIndex; // background color index
    protected int bgColor; // background color
    protected int lastBgColor; // previous bg color
    protected int pixelAspect; // pixel aspect ratio
    protected boolean lctFlag; // local color table flag
    protected boolean interlace; // interlace flag
    protected int lctSize; // local color table size
    protected int ix; // current image rectangle
    protected int iy; // current image rectangle
    protected int iw; // current image rectangle
    protected int ih; // current image rectangle
    protected int lrx;
    protected int lry;
    protected int lrw;
    protected int lrh;
    protected DixieMap image; // current frame
    protected DixieMap lastPixmap; // previous frame
    protected final byte[] block = new byte[256]; // current data block
    protected int blockSize = 0; // block size last graphic control extension info
    protected int dispose = 0; // 0=no action; 1=leave in place; 2=restore to bg; 3=restore to prev
    protected int lastDispose = 0;
    protected boolean transparency = false; // use transparent color
    protected int delay = 0; // delay in milliseconds
    protected int transIndex; // transparent color index
    // LZW decoder working arrays
    protected short[] prefix;
    protected byte[] suffix;
    protected byte[] pixelStack;
    protected byte[] pixels;
    protected Vector<GifFrame> frames; // frames read from current file
    protected int frameCount;

    /**
     * Gets the image contents of frame n.
     *
     * @return BufferedPixmap representation of frame, or null if n is invalid.
     */
    public DixieMap getFrame(int n) {
        if (frameCount <= 0)
            return null;
        n %= frameCount;
        return frames.elementAt(n).image;
    }

    /**
     * Reads GIF image from stream
     *
     * @param is containing GIF file.
     */
    public void read(InputStream is) {
        init();
        if (is != null) {
            in = is;
            readHeader();
            if (!err()) {
                readContents();
                if (frameCount < 0) {
                    status = STATUS_FORMAT_ERROR;
                }
            }
        } else {
            status = STATUS_OPEN_ERROR;
        }
        try {
            assert is != null;
            is.close();
        } catch (Exception ignored) {
        }
    }

    /**
     * Gets display duration for specified frame.
     *
     * @param n
     *          int index of frame
     * @return delay in milliseconds
     */
    public int getDelay(int n) {
        delay = -1;
        if ((n >= 0) && (n < frameCount)) {
            delay = frames.elementAt(n).delay;
        }
        return delay;
    }

    /**
     * Gets the number of frames read from file.
     *
     * @return frame count
     */
    public int getFrameCount() {
        return frameCount;
    }

    /**
     * Creates new frame image from current data (and previous frames as specified by their disposition codes).
     */
    protected void setPixels() {
        // expose destination image's pixels as int array
        int[] dest = new int[width * height];
        // fill in starting image contents based on last image's dispose code
        if (lastDispose > 0) {
            if (lastDispose == 3) {
                // use image before last
                int n = frameCount - 2;
                if (n > 0) {
                    lastPixmap = getFrame(n - 1);
                } else {
                    lastPixmap = null;
                }
            }
            if (lastPixmap != null) {
                lastPixmap.getPixels(dest, width, width, height);
                // copy pixels
                if (lastDispose == 2) {
                    // fill last image rect area with background color
                    int c = 0;
                    if (!transparency) {
                        c = lastBgColor;
                    }
                    for (int i = 0; i < lrh; i++) {
                        int n1 = (lry + i) * width + lrx;
                        int n2 = n1 + lrw;
                        for (int k = n1; k < n2; k++) {
                            dest[k] = c;
                        }
                    }
                }
            }
        }
        // copy each source line to the appropriate place in the destination
        int pass = 1;
        int inc = 8;
        int iline = 0;
        for (int i = 0; i < ih; i++) {
            int line = i;
            if (interlace) {
                if (iline >= ih) {
                    pass++;
                    switch (pass) {
                        case 2:
                            iline = 4;
                            break;
                        case 3:
                            iline = 2;
                            inc = 4;
                            break;
                        case 4:
                            iline = 1;
                            inc = 2;
                            break;
                        default:
                            break;
                    }
                }
                line = iline;
                iline += inc;
            }
            line += iy;
            if (line < height) {
                int k = line * width;
                int dx = k + ix; // start of line in dest
                int dlim = dx + iw; // end of dest line
                if ((k + width) < dlim) {
                    dlim = k + width; // past dest edge
                }
                int sx = i * iw; // start of line in source
                while (dx < dlim) {
                    // map color and insert in destination
                    int index = ((int) pixels[sx++]) & 0xff;
                    int c = act[index];
                    if (c != 0) {
                        dest[dx] = c;
                    }
                    dx++;
                }
            }
        }
        image = new DixieMap(dest, width, height);
        //Pixmap.createPixmap(dest, width, height, Config.ARGB_4444);
    }

    /**
     * Decodes LZW image data into pixel array. Adapted from John Cristy's BitmapMagick.
     */
    protected void decodeBitmapData() {
        int nullCode = -1;
        int npix = iw * ih;
        int available;
        int clear;
        int code_mask;
        int code_size;
        int end_of_information;
        int in_code;
        int old_code;
        int bits;
        int code;
        int count;
        int i;
        int datum;
        int data_size;
        int first;
        int top;
        int bi;
        int pi;
        if ((pixels == null) || (pixels.length < npix)) {
            pixels = new byte[npix]; // allocate new pixel array
        }
        if (prefix == null) {
            prefix = new short[MAX_STACK_SIZE];
        }
        if (suffix == null) {
            suffix = new byte[MAX_STACK_SIZE];
        }
        if (pixelStack == null) {
            pixelStack = new byte[MAX_STACK_SIZE + 1];
        }
        // Initialize GIF data stream decoder.
        data_size = read();
        clear = 1 << data_size;
        end_of_information = clear + 1;
        available = clear + 2;
        old_code = nullCode;
        code_size = data_size + 1;
        code_mask = (1 << code_size) - 1;
        for (code = 0; code < clear; code++) {
            prefix[code] = 0; // XXX ArrayIndexOutOfBoundsException
            suffix[code] = (byte) code;
        }
        // Decode GIF pixel stream.
        datum = bits = count = first = top = pi = bi = 0;
        for (i = 0; i < npix;) {
            if (top == 0) {
                if (bits < code_size) {
                    // Load bytes until there are enough bits for a code.
                    if (count == 0) {
                        // Read a new data block.
                        count = readBlock();
                        if (count <= 0) {
                            break;
                        }
                        bi = 0;
                    }
                    datum += (((int) block[bi]) & 0xff) << bits;
                    bits += 8;
                    bi++;
                    count--;
                    continue;
                }
                // Get the next code.
                code = datum & code_mask;
                datum >>= code_size;
                bits -= code_size;
                // Interpret the code
                if ((code > available) || (code == end_of_information)) {
                    break;
                }
                if (code == clear) {
                    // Reset decoder.
                    code_size = data_size + 1;
                    code_mask = (1 << code_size) - 1;
                    available = clear + 2;
                    old_code = nullCode;
                    continue;
                }
                if (old_code == nullCode) {
                    pixelStack[top++] = suffix[code];
                    old_code = code;
                    first = code;
                    continue;
                }
                in_code = code;
                if (code == available) {
                    pixelStack[top++] = (byte) first;
                    code = old_code;
                }
                while (code > clear) {
                    pixelStack[top++] = suffix[code];
                    code = prefix[code];
                }
                first = ((int) suffix[code]) & 0xff;
                // Add a new string to the string table,
                if (available >= MAX_STACK_SIZE) {
                    break;
                }
                pixelStack[top++] = (byte) first;
                prefix[available] = (short) old_code;
                suffix[available] = (byte) first;
                available++;
                if (((available & code_mask) == 0) && (available < MAX_STACK_SIZE)) {
                    code_size++;
                    code_mask += available;
                }
                old_code = in_code;
            }
            // Pop a pixel off the pixel stack.
            top--;
            pixels[pi++] = pixelStack[top];
            i++;
        }
        for (i = pi; i < npix; i++) {
            pixels[i] = 0; // clear missing pixels
        }
    }

    /**
     * Initializes or re-initializes reader
     */
    protected void init() {
        status = STATUS_OK;
        frameCount = 0;
        frames = new Vector<>();
        gct = null;
        lct = null;
    }

    /**
     * Reads next variable length block from input.
     *
     * @return number of bytes stored in "buffer"
     */
    protected int readBlock() {
        blockSize = read();
        int n = 0;
        if (blockSize > 0) {
            try {
                int count;
                while (n < blockSize) {
                    count = in.read(block, n, blockSize - n);
                    if (count == -1) {
                        break;
                    }
                    n += count;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (n < blockSize) {
                status = STATUS_FORMAT_ERROR;
            }
        }
        return n;
    }

    /**
     * Returns true if an error was encountered during reading/decoding
     */
    protected boolean err() {
        return status != STATUS_OK;
    }

    /**
     * Main file parser. Reads GIF content blocks.
     */
    protected void readContents() {
        // read GIF file content blocks
        boolean done = false;
        while (!(done || err())) {
            int code = read();
            switch (code) {
                case 0x2C: // image separator
                    readBitmap();
                    break;
                case 0x21: // extension
                    code = read();
                    switch (code) {
                        case 0xf9: // graphics control extension
                            readGraphicControlExt();
                            break;
                        case 0xff: // application extension
                            readBlock();
                            StringBuilder app = new StringBuilder();
                            for (int i = 0; i < 11; i++) {
                                app.append((char) block[i]);
                            }
                            if ("NETSCAPE2.0".equals(app.toString())) {
                                readNetscapeExt();
                            } else {
                                skip(); // don't care
                            }
                            break;
                        // comment extension
                        // plain text extension
                        default: // uninteresting extension
                            skip();
                    }
                    break;
                case 0x3b: // terminator
                    done = true;
                    break;
                case 0x00: // bad byte, but keep going and see what happens
                    break;
                default:
                    status = STATUS_FORMAT_ERROR;
            }
        }
    }

    /**
     * Reads a single byte from the input stream.
     */
    protected int read() {
        int curByte = 0;
        try {
            curByte = in.read();
        } catch (Exception e) {
            status = STATUS_FORMAT_ERROR;
        }
        return curByte;
    }

    /**
     * Reads GIF file header information.
     */
    protected void readHeader() {
        StringBuilder id = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            id.append((char) read());
        }
        if (!id.toString().startsWith("GIF")) {
            status = STATUS_FORMAT_ERROR;
            return;
        }
        readLSD();
        if (gctFlag && !err()) {
            gct = readColorTable(gctSize);
            bgColor = gct[bgIndex];
        }
    }

    /**
     * Reads color table as 256 RGB integer values
     *
     * @param ncolors int number of colors to read
     * @return int array containing 256 colors (packed ARGB with full alpha)
     */
    protected int[] readColorTable(int ncolors) {
        int nbytes = 3 * ncolors;
        int[] tab = null;
        byte[] c = new byte[nbytes];
        int n = 0;
        try {
            n = in.read(c);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (n < nbytes) {
            status = STATUS_FORMAT_ERROR;
        } else {
            tab = new int[256]; // max size to avoid bounds checks
            int i = 0;
            int j = 0;
            while (i < ncolors) {
                int r = ((int) c[j++]) & 0xff;
                int g = ((int) c[j++]) & 0xff;
                int b = ((int) c[j++]) & 0xff;
                tab[i++] = 0xff000000 | (r << 16) | (g << 8) | b;
            }
        }
        return tab;
    }

    public Animation<TextureRegion> getAnimation(PlayMode playMode) {
        int nrFrames = getFrameCount();
        Pixmap frame = getFrame(0);
        int width = frame.getWidth();
        int height = frame.getHeight();
        int vzones = (int) Math.sqrt(nrFrames);
        int hzones = vzones;

        while (vzones * hzones < nrFrames) vzones++;

        int v;
        int h;

        Pixmap target = new Pixmap(width * hzones, height * vzones, Pixmap.Format.RGBA8888);

        for (h = 0; h < hzones; h++) {
            for (v = 0; v < vzones; v++) {
                int frameID = v + h * vzones;
                if (frameID < nrFrames) {
                    frame = getFrame(frameID);
                    target.drawPixmap(frame, h * width, v * height);
                }
            }
        }

        Texture texture = new Texture(target);
        Array<TextureRegion> texReg = new Array<>();

        for (h = 0; h < hzones; h++) {
            for (v = 0; v < vzones; v++) {
                int frameID = v + h * vzones;
                if (frameID < nrFrames) {
                    TextureRegion tr = new TextureRegion(texture, h * width, v * height, width, height);
                    texReg.add(tr);
                }
            }
        }
        float frameDuration = (float) getDelay(0);
        frameDuration /= 1000; // convert milliseconds into seconds

        return new Animation<>(frameDuration, texReg, playMode);
    }

    /**
     * Reads Graphics Control Extension values
     */
    protected void readGraphicControlExt() {
        read(); // block size
        int packed = read(); // packed fields
        dispose = (packed & 0x1c) >> 2; // disposal method
        if (dispose == 0) {
            dispose = 1; // elect to keep old image if discretionary
        }
        transparency = (packed & 1) != 0;
        delay = readShort() * 10; // delay in milliseconds
        transIndex = read(); // transparent color index
        read(); // block terminator
    }

    /**
     * Reads next frame image
     */
    protected void readBitmap() {
        ix = readShort(); // (sub)image position & size
        iy = readShort();
        iw = readShort();
        ih = readShort();
        int packed = read();
        lctFlag = (packed & 0x80) != 0; // 1 - local color table flag interlace
        lctSize = (int) Math.pow(2, (packed & 0x07) + 1);
        // 3 - sort flag
        // 4-5 - reserved lctSize = 2 << (packed & 7); // 6-8 - local color
        // table size
        interlace = (packed & 0x40) != 0;
        if (lctFlag) {
            lct = readColorTable(lctSize); // read table
            act = lct; // make local table active
        } else {
            act = gct; // make global table active
            if (bgIndex == transIndex) {
                bgColor = 0;
            }
        }
        int save = 0;
        if (transparency) {
            save = act[transIndex];
            act[transIndex] = 0; // set transparent color if specified
        }
        if (act == null) {
            status = STATUS_FORMAT_ERROR; // no color table defined
        }
        if (err()) {
            return;
        }
        decodeBitmapData(); // decode pixel data
        skip();
        if (err()) {
            return;
        }
        frameCount++;
        // create new image to receive frame data
        image = new DixieMap(width, height);
        setPixels(); // transfer pixel data to image
        frames.addElement(new GifFrame(image, delay)); // add image to frame
        // list
        if (transparency) {
            act[transIndex] = save;
        }
        resetFrame();
    }

    private static class DixieMap extends Pixmap {
        DixieMap(int w, int h) {
            super(w, h, Format.RGBA8888);
        }

        DixieMap(int[] data, int w, int h) {
            super(w, h, Format.RGBA8888);

            int x;
            int y;

            for (y = 0; y < h; y++) {
                for (x = 0; x < w; x++) {
                    int pxl_ARGB8888 = data[x + y * w];
                    int pxl_RGBA8888 =
                            ((pxl_ARGB8888 >> 24) & 0x000000ff) | ((pxl_ARGB8888 << 8) & 0xffffff00);
                    // convert ARGB8888 > RGBA8888
                    drawPixel(x, y, pxl_RGBA8888);
                }
            }
        }

        private void getPixels(int[] pixels, int stride, int width, int height) {
            java.nio.ByteBuffer bb = getPixels();

            int k;
            int l;
            int offset = 0;

            for (k = 0; k < height; k++) {
                int _offset = offset;
                for (l = 0; l < width; l++) {
                    int pxl = bb.getInt(4 * (l + k * width));

                    // convert RGBA8888 > ARGB8888
                    pixels[_offset++] = ((pxl >> 8) & 0x00ffffff) | ((pxl << 24) & 0xff000000);
                }
                offset += stride;
            }
        }
    }

    /**
     * Reads Logical Screen Descriptor
     */
    protected void readLSD() {
        // logical screen size
        width = readShort();
        height = readShort();
        // packed fields
        int packed = read();
        gctFlag = (packed & 0x80) != 0; // 1 : global color table flag
        // 2-4 : color resolution
        // 5 : gct sort flag
        gctSize = 2 << (packed & 7); // 6-8 : gct size
        bgIndex = read(); // background color index
        pixelAspect = read(); // pixel aspect ratio
    }

    /**
     * Reads Netscape extenstion to obtain iteration count
     */
    protected void readNetscapeExt() {
        do {
            readBlock();
            if (block[0] == 1) {
                // loop count sub-block
                int b1 = ((int) block[1]) & 0xff;
                int b2 = ((int) block[2]) & 0xff;
                loopCount = (b2 << 8) | b1;
            }
        } while ((blockSize > 0) && !err());
    }

    /**
     * Reads next 16-bit value, LSB first
     */
    protected int readShort() {
        // read 16-bit value, LSB first
        return read() | (read() << 8);
    }

    /**
     * Resets frame state for reading next image.
     */
    protected void resetFrame() {
        lastDispose = dispose;
        lrx = ix;
        lry = iy;
        lrw = iw;
        lrh = ih;
        lastPixmap = image;
        lastBgColor = bgColor;
        dispose = 0;
        transparency = false;
        delay = 0;
        lct = null;
    }

    /**
     * Skips variable length blocks up to and including next zero length block.
     */
    protected void skip() {
        do {
            readBlock();
        } while ((blockSize > 0) && !err());
    }

    private static class GifFrame {
        public final DixieMap image;
        public final int delay;

        public GifFrame(DixieMap im, int del) {
            image = im;
            delay = del;
        }
    }

    public static Animation<TextureRegion> loadGIFAnimation(Animation.PlayMode playMode, InputStream is) {
        GifDecoder gdec = new GifDecoder();
        gdec.read(is);
        return gdec.getAnimation(playMode);
    }
}
