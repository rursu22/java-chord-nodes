import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class TextAnalyser {

    /**
	 * Finds out what the average word length within the text is
	 * @param text given task text
	 * @return average word length in the text
	 */

    int analyseAverageWordLength(String text) {
		String words[] = text.split("[, <>\\;\\:\\\"\'\\-\\+\\/\\=\\[\\]\\(\\)£$%^&*!?.\n]+");
		int sum = 0;
		for (String str : words) {
			sum += str.length();
		}
		int result = Math.round(sum / words.length);
		return result;
	}

    /**
	 * Finds out the total word count in the text
	 * @param text given task text
	 * @return total word count in the text
	 */

	int analyseWordCount(String text) {
		String words[] = text.split("[, <>\\;\\:\\\"\'\\-\\+\\/\\=\\[\\]\\(\\)£$%^&*!?.\n]+");
        // for(String word : words) {
        //     System.out.println(word);
        // }
		return words.length;
	}


    /**
	 * 
	 * @param text given text
	 * @return HashMap of the form "<word, frequency>" that contains the highest frequency word within the text
	 */
    HashMap<String, Integer> analyseMostFrequent(String text) {
		HashMap<String, Integer> result = new HashMap<String, Integer>();
		// We split the word when we encounter any peculiar character that is not a word
		String words[] = text.split("[, <>\\;\\:\\\"\'\\-\\+\\/\\=\\[\\]\\(\\)£$%^&*!?.\n]+");
		// Extra step required to remove the spaces in the file
		List<String> filteredWords = Arrays.stream(words)
                                       .map(String::trim)
                                       .filter(word -> !word.isEmpty())
                                       .collect(Collectors.toCollection(ArrayList::new));
		Collections.sort(filteredWords);
		// DEBUG: print all of the words in the string
		// for(String s : words) {
		// System.out.println(s);
		// }
		int highestFrequency = 0;
		int currentFrequency = 1;
		String mostFrequentWord = "";
		// Since we sorted the array, exact same words are going to be next to eachother,
		// so we can just count how many times we see a word in a row
		for (int i = 0; i < filteredWords.size() - 1; i++) {
			if (filteredWords.get(i).equals(filteredWords.get(i + 1))) {
				// If the word is the same as the next one, increment the counter
				currentFrequency++;
			} else {
				// If it's not, check if we've seen this word more times than the most frequent word
				if (currentFrequency > highestFrequency) {
					mostFrequentWord = filteredWords.get(i);
					highestFrequency = currentFrequency;
				}
				currentFrequency = 1;
			}
		}
		result.put(mostFrequentWord, highestFrequency);

		return result;
	}

    /**
	 * Generates the XML for the completed task
	 * @param name name of the file
	 * @param fileSize file's size in bytes
	 * @param mostFrequentWord most frequent word found in the text
	 * @param highestFrequency the word that is present the most in the text
	 * @param averageLength the average length of a word in the text
	 * @param wordCount the total word count of the text
	 * @return an XML file that contains all of the analysed data
	 */

	String generateXML(int fileSize, String mostFrequentWord, int highestFrequency, int averageLength,int wordCount) {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
					"<stats>\n" +
					"<type>text</type>\n" +
					"<size>" + fileSize + " B</size>\n" +
					"<mostFrequent>\""  + mostFrequentWord + "\"</mostFrequent>\n" +
					"<highestFrequencyAmount>" + highestFrequency + "</highestFrequencyAmount>\n" +
					"<wordCount>" + wordCount + "</wordCount>\n" +
					"<averageLength>" + averageLength + "</averageLength>\n" +
					"</stats>";
		return xml;
	}
}
