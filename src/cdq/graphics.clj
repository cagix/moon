(ns cdq.graphics)

(defprotocol Graphics
  (draw-tiled-map! [_ tiled-map color-setter]))

(defmulti draw!
  (fn [[k] _graphics]
    k))

(defn handle-draws! [graphics draws]
  (doseq [component draws
          :when component]
    (draw! component graphics)))
