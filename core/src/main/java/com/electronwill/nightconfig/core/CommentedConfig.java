package com.electronwill.nightconfig.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.electronwill.nightconfig.core.utils.StringUtils.split;

/**
 * A modifiable config that supports comments.
 *
 * @author TheElectronWill
 */
public interface CommentedConfig extends UnmodifiableCommentedConfig, Config {
	/**
	 * Sets a config comment.
	 *
	 * @param path    the comment's path, each part separated by a dot. Example "a.b.c"
	 * @param comment the comment to set
	 * @return the old comment if any, or {@code null}
	 */
	default String setComment(String path, String comment) {
		return setComment(split(path, '.'), comment);
	}

	/**
	 * Sets a config comment.
	 *
	 * @param path    the comment's path, each element of the list is a different part of the path.
	 * @param comment the comment to set
	 * @return the old comment if any, or {@code null}
	 */
	String setComment(List<String> path, String comment);

	/**
	 * Removes a comment from the config.
	 *
	 * @param path the comment's path, each part separated by a dot. Example "a.b.c"
	 * @return the old comment if any, or {@code null}
	 */
	default String removeComment(String path) {
		return removeComment(split(path, '.'));
	}

	/**
	 * Removes a comment from the config.
	 *
	 * @param path the comment's path, each element of the list is a different part of the path.
	 * @return the old comment if any, or {@code null}
	 */
	String removeComment(List<String> path);

	/**
	 * Removes all the comments from the config.
	 */
	void clearComments();

	/**
	 * Sets the comments of the config to the content of the specified Map. The Map isn't
	 * directly used, its content is copied.
	 *
	 * @param comments the comments to set
	 */
	default void setComments(Map<String, CommentNode> comments) {
		for (Map.Entry<String, CommentNode> entry : comments.entrySet()) {
			String key = entry.getKey();
			CommentNode node = entry.getValue();
			String comment = node.getComment();
			if (comment != null) {
				setComment(Collections.singletonList(key), comment);
			}
			Map<String, CommentNode> children = node.getChildren();
			if (children != null) {
				CommentedConfig config = get(Collections.singletonList(key));
				config.setComments(children);
			}
		}
	}

	/**
	 * Copies the comments of a config to this config.
	 *
	 * @param commentedConfig the config to copy its comments
	 */
	default void setComments(UnmodifiableCommentedConfig commentedConfig) {
		for (UnmodifiableCommentedConfig.Entry entry : commentedConfig.entrySet()) {
			String key = entry.getKey();
			String comment = entry.getComment();
			if (comment != null) {
				setComment(Collections.singletonList(key), comment);
			}
			Object value = entry.getValue();
			if (value instanceof UnmodifiableCommentedConfig) {
				CommentedConfig config = get(Collections.singletonList(key));
				config.setComments((UnmodifiableCommentedConfig)value);
			}

		}
	}

	@Override
	default UnmodifiableCommentedConfig unmodifiable() {
		return new UnmodifiableCommentedConfig() {
			@Override
			public <T> T get(List<String> path) {
				return CommentedConfig.this.get(path);
			}

			@Override
			public String getComment(List<String> path) {
				return CommentedConfig.this.getComment(path);
			}

			@Override
			public boolean contains(List<String> path) {
				return CommentedConfig.this.contains(path);
			}

			@Override
			public boolean containsComment(List<String> path) {
				return CommentedConfig.this.containsComment(path);
			}

			@Override
			public int size() {
				return CommentedConfig.this.size();
			}

			@Override
			public Map<String, Object> valueMap() {
				return Collections.unmodifiableMap(CommentedConfig.this.valueMap());
			}

			@Override
			public Map<String, String> commentMap() {
				return Collections.unmodifiableMap(CommentedConfig.this.commentMap());
			}

			@Override
			public Map<String, CommentNode> getComments() {
				return CommentedConfig.this.getComments();
			}

			@Override
			public Set<? extends Entry> entrySet() {
				return CommentedConfig.this.entrySet();
			}

			@Override
			public ConfigFormat<?, ?, ?> configFormat() {
				return CommentedConfig.this.configFormat();
			}
		};
	}

	default CommentedConfig checked() {
		return new CheckedCommentedConfig(this);
	}

	/**
	 * Returns a Map view of the config's comments. Any change to the map is reflected in the
	 * config and vice-versa.
	 * <p>
	 * The comment map contains only the comments of the direct elements of the configuration, not
	 * the comments of their sub-elements.
	 */
	@Override
	Map<String, String> commentMap();

	@Override
	Set<? extends Entry> entrySet();

	/**
	 * A modifiable commented config entry.
	 */
	interface Entry extends Config.Entry, UnmodifiableCommentedConfig.Entry {
		/**
		 * Sets the entry's comment.
		 *
		 * @param comment the comment to set, may contain several lines.
		 * @return the previous comment, or {@code null} if none.
		 */
		String setComment(String comment);

		/**
		 * Removes the entry's comment.
		 *
		 * @return the previous comment, or {@code null} if none.
		 */
		String removeComment();
	}

	/**
	 * Creates a CommentedConfig of the given format.
	 *
	 * @param format the config's format
	 * @return a new empty config
	 */
	static CommentedConfig of(
			ConfigFormat<? extends CommentedConfig, ? super CommentedConfig, ? super CommentedConfig> format) {
		return new SimpleCommentedConfig(format);
	}

	/**
	 * Creates a CommentedConfig with format {@link InMemoryFormat#defaultInstance()}.
	 *
	 * @return a new empty config
	 */
	static CommentedConfig inMemory() {
		return new SimpleCommentedConfig(InMemoryCommentedFormat.defaultInstance());
	}

	/**
	 * Creates a CommentedConfig backed by a Map. Any change to the map is reflected in the config
	 * and vice-versa.
	 *
	 * @param map    the Map to use
	 * @param format the config's format
	 * @return a new config backed by the map
	 */
	static CommentedConfig wrap(Map<String, Object> map, ConfigFormat<?, ?, ?> format) {
		return new SimpleCommentedConfig(map, format);
	}

	/**
	 * Creates a new CommentedConfig with the content of the given config. The returned config will
	 * have the same format as the copied config.
	 *
	 * @param config the config to copy
	 * @return a copy of the config
	 */
	static CommentedConfig copy(UnmodifiableConfig config) {
		return new SimpleCommentedConfig(config, config.configFormat());
	}

	/**
	 * Creates a new CommentedConfig with the content of the given config.
	 *
	 * @param config the config to copy
	 * @param format the config's format
	 * @return a copy of the config
	 */
	static CommentedConfig copy(UnmodifiableConfig config, ConfigFormat<?, ?, ?> format) {
		return new SimpleCommentedConfig(config, format);
	}

	/**
	 * Creates a new CommentedConfig with the content of the given config. The returned config will
	 * have the same format as the copied config.
	 *
	 * @param config the config to copy
	 * @return a copy of the config
	 */
	static CommentedConfig copy(UnmodifiableCommentedConfig config) {
		return new SimpleCommentedConfig(config, config.configFormat());
	}

	/**
	 * Creates a new CommentedConfig with the content of the given config.
	 *
	 * @param config the config to copy
	 * @param format the config's format
	 * @return a copy of the config
	 */
	static CommentedConfig copy(UnmodifiableCommentedConfig config, ConfigFormat<?, ?, ?> format) {
		return new SimpleCommentedConfig(config, format);
	}
}
