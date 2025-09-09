(ns cdq.tx.spawn-line)

(defn do!
  [[_ {:keys [start end duration color thick?]}]
   _ctx]
  [[:tx/spawn-effect
    start
    {:entity/line-render {:thick? thick? :end end :color color}
     :entity/delete-after-duration duration}]])
