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

(defmulti clicked-inventory-cell (fn [[k] cell]
                                   k))
(defmethod clicked-inventory-cell :default [_ cell])

(defmulti manual-tick (fn [[k]]
                        k))
(defmethod manual-tick :default [_])
