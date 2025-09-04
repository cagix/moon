(ns cdq.graphics)

(defmulti draw!
  (fn [[k] _graphics]
    k))

(defn handle-draws! [graphics draws]
  (doseq [component draws
          :when component]
    (draw! component graphics)))
