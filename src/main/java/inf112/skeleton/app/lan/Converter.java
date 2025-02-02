package inf112.skeleton.app.lan;

import inf112.skeleton.app.cards.ProgramCard;
import inf112.skeleton.app.enums.Messages;
import inf112.skeleton.app.enums.Rotate;

import java.util.ArrayList;

/**
 * Convert from a string to a programcard and vica verca.
 * Used to send cards between sockets.
 */
public class Converter {

    /**
     * Give players player number and program card, return info as a string.
     * @param playerNumber number of player
     * @param programCard program card to convert
     * @return player number and info about players card in string
     */
    public String convertToString(int playerNumber, ProgramCard programCard) {
        if (programCard == null) {
            System.out.println("no card");
            return "no card";
        }
        StringBuilder string = new StringBuilder();
        String player = String.valueOf(playerNumber);
        String prio = String.valueOf(programCard.getPriority());
        String steps = String.valueOf(programCard.getDistance());
        String rotation = String.valueOf(programCard.getRotate());
        String name = programCard.getName();
        string.append(player).append(" ");
        string.append(prio).append(" ");
        string.append(steps).append(" ");
        string.append(rotation).append(" ");
        string.append(name);
        return string.toString();
    }

    /**
     *
     * @param programCard program card to convert
     * @return program card as a string
     */
    public String convertToString(ProgramCard programCard) {
        StringBuilder string = new StringBuilder();
        String prio = String.valueOf(programCard.getPriority());
        String steps = String.valueOf(programCard.getDistance());
        String rotation = String.valueOf(programCard.getRotate());
        String name = programCard.getName();
        string.append(prio).append(" ");
        string.append(steps).append(" ");
        string.append(rotation).append(" ");
        string.append(name);
        return string.toString();
    }

    /**
     * Convert a string to a corresponding program card.
     * @param string program card received from client
     * @return program card
     * @throws NotProgramCardException if string is not a program card
     */
    public ProgramCard convertToCard(String string) throws NotProgramCardException {
        ArrayList<String> strings = splitBySpace(string);
        try {
            int prio = Integer.parseInt(strings.get(0));
            int steps = Integer.parseInt(strings.get(1));
            Rotate rotation = getRotation(string);
            String name = getName(string);
            assert rotation != null;
            if (!string.equals(prio + " " + steps + " " + rotation + " " + name)) {
                throw new NotProgramCardException("This is not a program card: " +string);
            }
            return new ProgramCard(prio, steps, rotation, name);
        } catch (NumberFormatException error) {
            throw new NotProgramCardException("This is not a program card: "+string);
        }
    }

    /**
     * Put words separated by spaces in a list.
     * So "This is an example" becomes ["This", "is", "an", "example"].
     * @param string to split
     * @return ArrayList<String> List of string with index at spaces.
     */
    public ArrayList<String> splitBySpace(String string) {
        ArrayList<String> strings = new ArrayList<>();
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            Character character = string.charAt(i);
            if (character == ' ') {
                strings.add(str.toString());
                str = new StringBuilder();
            } else if (i == string.length()-1) {
                str.append(character);
                strings.add(str.toString());
            } else {
                str.append(character);
            }
        }
        return strings;
    }

    /**
     *
     * @param string to get name from
     * @return name of program card
     */
    private String getName(String string) {
        if (string.contains("U-turn")) {
            return "U-turn";
        } else if (string.contains("Left turn")) {
            return "Left turn";
        } else if (string.contains("Right turn")) {
            return "Right turn";
        } else if (string.contains("Right rotate")) {
            return "Right rotate";
        } else if (string.contains("Left rotate")) {
            return "Left rotate";
        } else if (string.contains("Move 3")) {
            return "Move 3";
        } else if (string.contains("Move 1")) {
            return "Move 1";
        } else if (string.contains("Move 2")) {
            return "Move 2";
        } else if (string.contains("Back up")) {
            return "Back up";
        } else {
            return "Could not match name.";
        }
    }


    /**
     * Get the rotation
     * @param message to interpret
     * @return rotation
     */
    private Rotate getRotation(String message) {
        if (message.contains("UTURN")) {
            return Rotate.UTURN;
        }
        if (message.contains("LEFT")) {
            return Rotate.LEFT;
        }
        if (message.contains("RIGHT")) {
            return Rotate.RIGHT;
        }
        if (message.contains("NONE")) {
            return Rotate.NONE;
        }
        return null;
    }

    /**
     *
     * @param message to interpret
     * @return true if message is attached to a player
     */
    public boolean isMessageFromAnotherPlayer(String message) {
        return Character.isDigit(message.charAt(0));
    }

    /**
     * Playernumber is always first in the message. Get the playernumber from this
     * message. {@link #isMessageFromAnotherPlayer(String)} needs to be true.
     *
     * @param message to interpret
     * @return the playerNumber for the player sending this message. Return -1 if the message is not attached to a
     * player.
     */
    public int getPlayerNumberFromMessage(String message) {
        if (isMessageFromAnotherPlayer(message)) {
            return Character.getNumericValue(message.charAt(0));
        }
        return -1;
    }

    /**
     *
     * @param message to interpret
     * @return the message that a player sent you. {@link #isMessageFromAnotherPlayer(String)} needs to be true.
     */
    public String getMessageFromPlayer(String message) {
        if (isMessageFromAnotherPlayer(message)) {
            return message.substring(2);
        }
        return null;
    }

    /**
     *
     * @param playerNumber player which sends message
     * @param message content of message
     * @return message with player attached
     */
    public String createMessageFromPlayer(int playerNumber, Messages message) {
        return playerNumber + " " + message.toString();
    }

    public String createQuitMessage(int myPlayerNumber) {
        return (myPlayerNumber + " " + Messages.QUIT);
    }

    public String getMapPath(String message) {
        return message.substring(Messages.HERE_IS_MAP.toString().length());
    }

    public String createMapPathMessage(String mapPath) {
        return Messages.HERE_IS_MAP +mapPath;
    }

    public String createNumberOfPlayersMessage(int numberOfPlayers) {
        return Messages.PLAYERS.toString()+numberOfPlayers;
    }

    public int getNumbersOfPlayers(String message) {
        return Integer.parseInt(message.substring(Messages.PLAYERS.toString().length()));
    }

    public String createPlayerNumberMessage(int playerNumber) {
        return Messages.YOUR_NUMBER.toString()+playerNumber;
    }

    public int getMyPlayerNumber(String message) {
        return Integer.parseInt(message.substring(Messages.YOUR_NUMBER.toString().length()));
    }
}
