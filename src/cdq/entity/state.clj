(ns cdq.entity.state)

(defmulti cursor (fn [[k]]
                   k))
(defmethod cursor :default [_])

(defmulti pause-game? (fn [[k]]
                        k))

(defmulti enter! (fn [[k] _context]
                  k))
(defmethod enter! :default [_ _context])

(defmulti exit! (fn [[k] _context]
                  k))
(defmethod exit! :default [_ _context])
