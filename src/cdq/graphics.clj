(ns cdq.graphics)

(defprotocol Graphics
  (draw-on-world-viewport! [_ f])
  (draw-tiled-map! [_ tiled-map color-setter])
  (ui-viewport-height [_])
  (handle-draws! [_ draws]))

(defmulti draw!
  (fn [[k] _graphics]
    k))

(defn handle-draws! [graphics draws]
  (doseq [component draws
          :when component]
    (draw! component graphics)))
