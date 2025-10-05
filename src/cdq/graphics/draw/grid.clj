(ns cdq.graphics.draw.grid
  (:require [cdq.graphics.draws :as draws]))

(defn do!
  [graphics leftx bottomy gridw gridh cellw cellh color]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (draws/handle! graphics
                     [[:draw/line [linex topy] [linex bottomy] color]]))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (draws/handle! graphics
                     [[:draw/line [leftx liney] [rightx liney] color]]))))
