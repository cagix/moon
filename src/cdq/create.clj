(ns cdq.create)

(defn error* [_context]
  nil)

(defn tiled-map* [context]
  (:tiled-map (:cdq.context/level context)))

(defn entity-ids* [_context]
  (atom {}))

(defn factions-iterations* [config _context]
  config)
