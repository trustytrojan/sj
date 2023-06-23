package simple_json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JsonParser {
	private JsonParser() {}

	private static class JsonParserException extends RuntimeException {
		JsonParserException(String message) {
			super(message);
		}
	}

	static Object parse(List<Token> tokens) {
		if (tokens.size() == 0) {
			throw new JsonParserException("Tokens list is empty");
		}

		final var firstToken = tokens.get(0);

		return switch (firstToken) {
			case final StructuralToken st -> switch (st) {
				case LEFT_BRACKET -> parseArray(tokens);
				case LEFT_BRACE -> parseObject(tokens);
				default -> throw new JsonParserException("what the hell?");
			};

			case final Value v -> v.value;

			default -> throw new JsonParserException("what the hell?");
		};
	}

	static List<Object> parseArray(List<Token> tokens) {
		if (!representsArray(tokens)) {
			throw new JsonParserException("Not a JSON array");
		}

		if (tokens.size() == 2) {
			return Collections.emptyList();
		}

		tokens = withoutFirstAndLastTokens(tokens);

		final var size = tokens.size();
		final var array = new ArrayList<>();

		for (var i = 0; i < size; ++i) {
			final var j = indexOfNextNonNestedComma(tokens, i);
			array.add(parse(tokens.subList(i, j)));
			i = j;
		}

		return Collections.unmodifiableList(array);
	}

	static Map<String, Object> parseObject(List<Token> tokens) {
		if (!representsObject(tokens)) {
			throw new JsonParserException("Not a JSON object");
		}

		if (tokens.size() == 2) {
			return Collections.emptyMap();
		}

		tokens = withoutFirstAndLastTokens(tokens);

		final var size = tokens.size();
		final var map = new HashMap<String, Object>();

		for (var i = 0; i < size; ++i) {
			final var j = indexOfNextNonNestedComma(tokens, i);
			final var entry = parseObjectEntry(tokens.subList(i, j));
			map.put(entry.getKey(), entry.getValue());
			i = j;
		}

		return Collections.unmodifiableMap(map);
	}

	private static boolean representsObject(List<Token> tokens) {
		final var firstTokenIsLeftBrace = tokens.get(0) == StructuralToken.LEFT_BRACE;
		final var lastTokenIsRightBrace = tokens.get(tokens.size() - 1) == StructuralToken.RIGHT_BRACE;
		return firstTokenIsLeftBrace && lastTokenIsRightBrace;
	}

	private static boolean representsArray(List<Token> tokens) {
		final var firstTokenIsLeftBracket = tokens.get(0) == StructuralToken.LEFT_BRACKET;
		final var lastTokenIsRightBracket = tokens.get(tokens.size() - 1) == StructuralToken.RIGHT_BRACKET;
		return firstTokenIsLeftBracket && lastTokenIsRightBracket;
	}

	private static List<Token> withoutFirstAndLastTokens(List<Token> tokens) {
		return tokens.subList(1, tokens.size() - 1);
	}

	/**
	 * Searches this token list for a token of type {@code COMMA} at the same
	 * container depth as the token at {@code startIndex}.
	 * 
	 * @param tokens list of tokens to search
	 * @return the index of the first {@code COMMA} token found, otherwise
	 *         {@code tokens.size()}
	 */
	private static int indexOfNextNonNestedComma(List<Token> tokens, int startIndex) {
		final var size = tokens.size();
		var depth = 0;
		for (var i = startIndex; i < size; ++i) {
			if (depth < 0) {
				throw new JsonParserException("Invalid JSON container: depth < 0");
			}
			if (tokens.get(i) instanceof final StructuralToken st) {
				switch (st) {
					case LEFT_BRACE -> ++depth;
					case LEFT_BRACKET -> ++depth;
					case RIGHT_BRACE -> --depth;
					case RIGHT_BRACKET -> --depth;
					case COMMA -> {
						if (depth == 0) {
							return i;
						}
					}
					default -> {}
				}
			}
		}
		return size;
	}

	private static Map.Entry<String, Object> parseObjectEntry(List<Token> tokens) {
		final var size = tokens.size();

		if (size < 3) {
			throw new JsonParserException("Invalid JSON object entry: size < 3");
		}

		if (!(((Value) tokens.get(0)).value instanceof final String key)) {
			throw new JsonParserException("Invalid JSON object entry: key is not a string");
		} else if (tokens.get(1) != StructuralToken.COLON) {
			throw new JsonParserException("Invalid JSON object entry: no colon present");
		}

		return Map.entry(key, parse(tokens.subList(2, size)));
	}

	/*
	 * private static String parseString(String s) {
	 * final var length = s.length();
	 * 
	 * if (s.charAt(0) != '"' || s.charAt(length - 1) != '"') {
	 * throw new JsonParserException("Input is not a valid JSON string!");
	 * }
	 * 
	 * final var sb = new StringBuilder();
	 * 
	 * for (int i = 1; i < length - 1; ++i) {
	 * switch (s.charAt(i)) {
	 * case '\\' ->
	 * sb.append(switch (s.charAt(++i)) {
	 * case '"' -> '"';
	 * case '\\' -> '\\';
	 * case '/' -> '/';
	 * case 'b' -> '\b';
	 * case 'f' -> '\f';
	 * case 'n' -> '\n';
	 * case 'r' -> '\r';
	 * case 't' -> '\t';
	 * //case 'u' -> Character.toChars(Integer.parseInt(s.substring(i + 2, i + 6),
	 * 16))[0];
	 * default -> throw new
	 * JsonParserException("Input is not a valid JSON string!");
	 * });
	 * case '"' -> throw new JsonParserException("Input is not a valid JSON
	 * string!");
	 * default -> sb.append(s.charAt(i));
	 * }
	 * }
	 * 
	 * return sb.toString();
	 * }
	 */
}