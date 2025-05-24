(ns cdq.math)

(defn circle->outer-rectangle [{[x y] :position :keys [radius]}]
  (let [radius (float radius)
        size (* radius 2)]
    {:left-bottom [(- (float x) radius)
                   (- (float y) radius)]
     :width  size
     :height size}))

(defn rectangle->tiles
  [{[x y] :left-bottom :keys [left-bottom width height]}]
  {:pre [left-bottom width height]}
  (let [x       (float x)
        y       (float y)
        width   (float width)
        height  (float height)
        l (int x)
        b (int y)
        r (int (+ x width))
        t (int (+ y height))]
    (set
     (if (or (> width 1) (> height 1))
       (for [x (range l (inc r))
             y (range b (inc t))]
         [x y])
       [[l b] [l t] [r b] [r t]]))))
