(ns cdq.g.graphics
  (:require [cdq.draw :as draw]
            [cdq.g :as g]
            [gdl.graphics.batch :as batch]))

(defmulti draw! (fn [[k] _ctx]
                  k))

(defmethod draw! :draw/text [[_ opts] ctx]
  (draw/text ctx opts))

(defmethod draw! :draw/rectangle [[_ & opts] ctx]
  (apply draw/rectangle ctx opts))

(defmethod draw! :draw/filled-rectangle [[_ & opts] ctx]
  (apply draw/filled-rectangle ctx opts))

(defmethod draw! :draw/rotated-centered [[_ & opts] ctx]
  (apply draw/rotated-centered ctx opts))

(defmethod draw! :draw/centered [[_ & opts] ctx]
  (apply draw/centered ctx opts))

(defmethod draw! :draw/image [[_ & opts] ctx]
  (apply draw/image ctx opts))

(defmethod draw! :draw/line [[_ & opts] ctx]
  (apply draw/line ctx opts))

(defmethod draw! :draw/circle [[_ & opts] ctx]
  (apply draw/circle ctx opts))

(defmethod draw! :draw/filled-circle [[_ & opts] ctx]
  (apply draw/filled-circle ctx opts))

(defmethod draw! :draw/ellipse [[_ & opts] ctx]
  (apply draw/ellipse ctx opts))

(defmethod draw! :draw/sector [[_ & opts] ctx]
  (apply draw/sector ctx opts))

(defmethod draw! :draw/grid [[_ & opts] ctx]
  (apply draw/grid ctx opts))

(defmethod draw! :draw/with-line-width [[_ width draws] ctx]
  (draw/with-line-width ctx width
    (fn []
      (g/handle-draws! ctx draws))))

(extend-type cdq.g.Game
  g/Graphics
  (handle-draws! [ctx draws]
    (doseq [component draws
            :when component]
      (draw! component ctx)))

  (draw-on-world-viewport! [{:keys [ctx/batch
                                    ctx/world-viewport
                                    ctx/world-unit-scale]
                             :as ctx} fns]
    (batch/draw-on-viewport! batch
                             world-viewport
                             (fn []
                               (draw/with-line-width ctx world-unit-scale
                                 (fn []
                                   (doseq [f fns]
                                     (f (assoc ctx :ctx/unit-scale world-unit-scale)))))))))
