class RoomSearchCache {
  RoomSearchCache._();

  static final RoomSearchCache instance = RoomSearchCache._();

  static const Duration _ttl = Duration(seconds: 15);
  final Map<String, _CacheEntry> _entries = <String, _CacheEntry>{};

  List<dynamic>? read(String key) {
    final entry = _entries[key];
    if (entry == null) return null;

    if (DateTime.now().difference(entry.createdAt) > _ttl) {
      _entries.remove(key);
      return null;
    }

    return List<dynamic>.from(entry.results);
  }

  void write(String key, List<dynamic> results) {
    _entries[key] = _CacheEntry(
      createdAt: DateTime.now(),
      results: List<dynamic>.from(results),
    );
  }

  void invalidate() => _entries.clear();
}

class _CacheEntry {
  const _CacheEntry({
    required this.createdAt,
    required this.results,
  });

  final DateTime createdAt;
  final List<dynamic> results;
}
