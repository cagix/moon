(ns cdq.create.shape-drawer
  (:require cdq.graphics.color
            cdq.graphics.shape-drawer
            cdq.graphics.2d.texture-region
            clojure.gdx.interop
            cdq.math.utils))

(defn create [{:keys [cdq.graphics/batch
                      cdq.graphics/shape-drawer-texture]}]
  (space.earlygrey.shapedrawer.ShapeDrawer. batch
                                            (cdq.graphics.2d.texture-region/create shape-drawer-texture 1 0 1 1)))

(defn- munge-color ^com.badlogic.gdx.graphics.Color [c]
  (cond (= com.badlogic.gdx.graphics.Color (class c)) c
        (keyword? c) (clojure.gdx.interop/k->color c)
        (vector? c) (apply cdq.graphics.color/create c)
        :else (throw (ex-info "Cannot understand color" c))))

(defn- set-color [shape-drawer color]
  (space.earlygrey.shapedrawer.ShapeDrawer/.setColor shape-drawer (munge-color color)))

(extend-type space.earlygrey.shapedrawer.ShapeDrawer
  cdq.graphics.shape-drawer/ShapeDrawer
  (ellipse [this [x y] radius-x radius-y color]
    (set-color this color)
    (.ellipse this
              (float x)
              (float y)
              (float radius-x)
              (float radius-y)))

  (filled-ellipse [this [x y] radius-x radius-y color]
    (set-color this color)
    (.filledEllipse this
                    (float x)
                    (float y)
                    (float radius-x)
                    (float radius-y)))

  (circle [this [x y] radius color]
    (set-color this color)
    (.circle this
             (float x)
             (float y)
             (float radius)))

  (filled-circle [this [x y] radius color]
    (set-color this color)
    (.filledCircle this
                   (float x)
                   (float y)
                   (float radius)))

  (arc [this [center-x center-y] radius start-angle degree color]
    (set-color this color)
    (.arc this
          (float center-x)
          (float center-y)
          (float radius)
          (float (cdq.math.utils/degree->radians start-angle))
          (float (cdq.math.utils/degree->radians degree))))

  (sector [this [center-x center-y] radius start-angle degree color]
    (set-color this color)
    (.sector this
             (float center-x)
             (float center-y)
             (float radius)
             (float (cdq.math.utils/degree->radians start-angle))
             (float (cdq.math.utils/degree->radians degree))))

  (rectangle [this x y w h color]
    (set-color this color)
    (.rectangle this
                (float x)
                (float y)
                (float w)
                (float h)))

  (filled-rectangle [this x y w h color]
    (set-color this color)
    (.filledRectangle this
                      (float x)
                      (float y)
                      (float w)
                      (float h)))

  (line [this [sx sy] [ex ey] color]
    (set-color this color)
    (.line this
           (float sx)
           (float sy)
           (float ex)
           (float ey)))

  (with-line-width [this width draw-fn]
    (let [old-line-width (.getDefaultLineWidth this)]
      (.setDefaultLineWidth this (float (* width old-line-width)))
      (draw-fn)
      (.setDefaultLineWidth this (float old-line-width)))))
