public class CSVAnalyser {
    int[] countColsAndRows(String text) {
        // We split the csv file by delimiter so that we can use the endline as the end of the row
        String[] words = text.split("[;]");
        // We also save the count for every word so that we can get the rows based on it
        String[] allWords = text.split("[;\n]");
        int results[] = new int[2];
        int cols = 0;
        
        for(String word : words) {
            if (!word.contains("\n")) {
                cols++;
            } else {
                cols++;
                break;
            }
        }
        // The number of rows is just the number of all cells - the columns
        int rows = allWords.length - cols;
        results[0] = cols;
        results[1] = rows;
        return results;
    }

    int[] numericalAndTextCols(String text) {
        String[] words = text.split("[;]");
        int numericalColumns = 0;
        int textColumns = 0;
        int results[] = new int[2];

        for(String word : words) {
            // We use a regular expression to find out if the column contains any text. If it does, then it's a text column, 
            // otherwise, it's a numerical column
            if(!word.matches("[A-Za-z]+")) {
                numericalColumns++;
            } else {
                textColumns++;
            }
            if(word.contains("\n")) {
                break;
            }
        }

        results[0] = numericalColumns;
        results[1] = textColumns;
        return results;
    }

    String generateXML(int fileSize, int numberOfRows, int numberOfCols, int numericalCols,int textCols) {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
					 "<stats>\n" +
                     "<type>csv</type>\n" +
					 "<size>" + fileSize + " B</size>\n" +
					 "<numberOfRows>"  + numberOfRows + "</numberOfRows>\n" +
					 "<numberOfCols>" + numberOfCols + "</numberOfCols>\n" +
					 "<textCols>" + textCols + "</textCols>\n" +
					 "<numericalCols>" + numericalCols + "</numericalCols>\n" +
					 "</stats>";

		return xml;
	}
}
