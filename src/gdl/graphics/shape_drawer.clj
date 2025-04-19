(ns gdl.graphics.shape-drawer)

(defprotocol ShapeDrawer
  (ellipse [_ [x y] radius-x radius-y color])
  (filled-ellipse [_ [x y] radius-x radius-y color])
  (circle [_ [x y] radius color])
  (filled-circle [_ [x y] radius color])
  (arc [_ [center-x center-y] radius start-angle degree color])
  (sector [_ [center-x center-y] radius start-angle degree color])
  (rectangle [_ x y w h color])
  (filled-rectangle [_ x y w h color])
  (line [_ [sx sy] [ex ey] color])
  (with-line-width [_ width draw-fn]))

(defn grid [shape-drawer leftx bottomy gridw gridh cellw cellh color]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (line shape-drawer [linex topy] [linex bottomy] color))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (line shape-drawer [leftx liney] [rightx liney] color))))
