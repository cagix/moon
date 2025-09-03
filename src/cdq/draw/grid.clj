(ns cdq.draw.grid
  (:require [cdq.ctx.graphics :as graphics]))

(defn draw!
  [[_ leftx bottomy gridw gridh cellw cellh color]
   graphics]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (graphics/draw! [:draw/line [linex topy] [linex bottomy] color] graphics))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (graphics/draw! [:draw/line [leftx liney] [rightx liney] color] graphics))))
