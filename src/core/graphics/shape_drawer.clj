(ns ^:no-doc core.graphics.shape-drawer
  (:require [core.ctx :refer :all])
  (:import com.badlogic.gdx.math.MathUtils
           (com.badlogic.gdx.graphics Color Texture Pixmap Pixmap$Format)
           com.badlogic.gdx.graphics.g2d.TextureRegion
           space.earlygrey.shapedrawer.ShapeDrawer))

(defn ->build [batch]
  (let [tex (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                           (.setColor ^Color Color/WHITE)
                           (.drawPixel 0 0))
                  tex (Texture. pixmap)]
              (.dispose pixmap)
              tex)]
    {:shape-drawer (ShapeDrawer. batch (TextureRegion. tex 1 0 1 1))
     :shape-drawer-texture tex}))

(defn- degree->radians [degree]
  (* (float degree) MathUtils/degreesToRadians))

(defn- munge-color ^Color [color]
  (if (= Color (class color))
    color
    (apply ->color color)))

(defn- set-color [^ShapeDrawer shape-drawer color]
  (.setColor shape-drawer (munge-color color)))

(extend-type core.ctx.Graphics
  core.ctx/PShapeDrawer
  (draw-ellipse [{:keys [^ShapeDrawer shape-drawer]} [x y] radius-x radius-y color]
    (set-color shape-drawer color)
    (.ellipse shape-drawer (float x) (float y) (float radius-x) (float radius-y)) )

  (draw-filled-ellipse [{:keys [^ShapeDrawer shape-drawer]} [x y] radius-x radius-y color]
    (set-color shape-drawer color)
    (.filledEllipse shape-drawer (float x) (float y) (float radius-x) (float radius-y)))

  (draw-circle [{:keys [^ShapeDrawer shape-drawer]} [x y] radius color]
    (set-color shape-drawer color)
    (.circle shape-drawer (float x) (float y) (float radius)))

  (draw-filled-circle [{:keys [^ShapeDrawer shape-drawer]} [x y] radius color]
    (set-color shape-drawer color)
    (.filledCircle shape-drawer (float x) (float y) (float radius)))

  (draw-arc [{:keys [^ShapeDrawer shape-drawer]} [centre-x centre-y] radius start-angle degree color]
    (set-color shape-drawer color)
    (.arc shape-drawer centre-x centre-y radius (degree->radians start-angle) (degree->radians degree)))

  (draw-sector [{:keys [^ShapeDrawer shape-drawer]} [centre-x centre-y] radius start-angle degree color]
    (set-color shape-drawer color)
    (.sector shape-drawer centre-x centre-y radius (degree->radians start-angle) (degree->radians degree)))

  (draw-rectangle [{:keys [^ShapeDrawer shape-drawer]} x y w h color]
    (set-color shape-drawer color)
    (.rectangle shape-drawer x y w h) )

  (draw-filled-rectangle [{:keys [^ShapeDrawer shape-drawer]} x y w h color]
    (set-color shape-drawer color)
    (.filledRectangle shape-drawer (float x) (float y) (float w) (float h)) )

  (draw-line [{:keys [^ShapeDrawer shape-drawer]} [sx sy] [ex ey] color]
    (set-color shape-drawer color)
    (.line shape-drawer (float sx) (float sy) (float ex) (float ey)))

  (draw-grid [this leftx bottomy gridw gridh cellw cellh color]
    (let [w (* (float gridw) (float cellw))
          h (* (float gridh) (float cellh))
          topy (+ (float bottomy) (float h))
          rightx (+ (float leftx) (float w))]
      (doseq [idx (range (inc (float gridw)))
              :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
        (draw-line this [linex topy] [linex bottomy] color))
      (doseq [idx (range (inc (float gridh)))
              :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
        (draw-line this [leftx liney] [rightx liney] color))))

  (with-shape-line-width [{:keys [^ShapeDrawer shape-drawer]} width draw-fn]
    (let [old-line-width (.getDefaultLineWidth shape-drawer)]
      (.setDefaultLineWidth shape-drawer (float (* (float width) old-line-width)))
      (draw-fn)
      (.setDefaultLineWidth shape-drawer (float old-line-width)))))
