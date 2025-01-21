(ns cdq.effect)

(defmulti applicable? (fn [[k] effect-ctx]
                        k))

(defmulti handle (fn [[k] effect-ctx context]
                   k))

(defmulti useful? (fn [[k] effect-ctx context]
                    k))
(defmethod useful? :default [_ _effect-ctx context] true)
