package snaq.util;

/**
 * Interface for objects that can be referenced in a cache manager.
 * @see snaq.util.CacheManager
 * @author Giles Winstanley
 */
public interface Cacheable
{
	/**
	 * Uniquely identifies the object in the cache.
	 */
	public Object getId();

	/**
	 * Whether the item has expired.
	 */
	public boolean isExpired();
}