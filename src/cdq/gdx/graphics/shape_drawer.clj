(ns cdq.gdx.graphics.shape-drawer
  (:import (com.badlogic.gdx.graphics Color)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defn create [batch texture-region]
  (ShapeDrawer. batch texture-region))

(defn ellipse! [shape-drawer [x y] radius-x radius-y color]
  (ShapeDrawer/.setColor shape-drawer ^Color color)
  (ShapeDrawer/.ellipse shape-drawer
                        (float x)
                        (float y)
                        (float radius-x)
                        (float radius-y)))

(defn filled-ellipse! [shape-drawer [x y] radius-x radius-y color]
  (ShapeDrawer/.setColor shape-drawer ^Color color)
  (ShapeDrawer/.filledEllipse shape-drawer
                              (float x)
                              (float y)
                              (float radius-x)
                              (float radius-y)))

(defn circle! [shape-drawer [x y] radius color]
  (ShapeDrawer/.setColor shape-drawer ^Color color)
  (ShapeDrawer/.circle shape-drawer
                       (float x)
                       (float y)
                       (float radius)))

(defn filled-circle! [shape-drawer [x y] radius color]
  (ShapeDrawer/.setColor shape-drawer ^Color color)
  (ShapeDrawer/.filledCircle shape-drawer
                             (float x)
                             (float y)
                             (float radius)))

(defn rectangle! [shape-drawer x y w h color]
  (ShapeDrawer/.setColor shape-drawer ^Color color)
  (ShapeDrawer/.rectangle shape-drawer
                          (float x)
                          (float y)
                          (float w)
                          (float h)))

(defn filled-rectangle! [shape-drawer x y w h color]
  (ShapeDrawer/.setColor shape-drawer ^Color color)
  (ShapeDrawer/.filledRectangle shape-drawer
                                (float x)
                                (float y)
                                (float w)
                                (float h)))

; TODO now radians
(defn arc! [shape-drawer [center-x center-y] radius start-angle degree color]
  (ShapeDrawer/.setColor shape-drawer ^Color color)
  (ShapeDrawer/.arc shape-drawer
                    (float center-x)
                    (float center-y)
                    (float radius)
                    (float start-angle)
                    (float degree)))

; TODO now radians
(defn sector! [shape-drawer [center-x center-y] radius start-angle degree color]
  (ShapeDrawer/.setColor shape-drawer ^Color color)
  (ShapeDrawer/.sector shape-drawer
                       (float center-x)
                       (float center-y)
                       (float radius)
                       (float start-angle)
                       (float degree)))

(defn line! [shape-drawer [sx sy] [ex ey] color]
  (ShapeDrawer/.setColor shape-drawer ^Color color)
  (ShapeDrawer/.line shape-drawer
                     (float sx)
                     (float sy)
                     (float ex)
                     (float ey)))

(defn grid! [shape-drawer leftx bottomy gridw gridh cellw cellh color]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (line! shape-drawer [linex topy] [linex bottomy] color))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (line! shape-drawer [leftx liney] [rightx liney] color ))))

(defn with-line-width [^ShapeDrawer this width draw-fn]
  (let [old-line-width (.getDefaultLineWidth this)]
    (.setDefaultLineWidth this (float (* width old-line-width)))
    (draw-fn)
    (.setDefaultLineWidth this (float old-line-width))))
