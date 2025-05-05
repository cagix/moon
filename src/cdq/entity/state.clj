(ns cdq.entity.state)

(defmulti cursor (fn [[k]]
                   k))
(defmethod cursor :default [_])

(defmulti pause-game? (fn [[k]]
                        k))

(defmulti enter! (fn [[k]]
                  k))
(defmethod enter! :default [_])

(defmulti exit! (fn [[k]]
                  k))
(defmethod exit! :default [_])
