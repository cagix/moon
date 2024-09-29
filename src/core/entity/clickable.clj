(in-ns 'core.entity)

(defcomponent :entity/clickable
  (render [[_ {:keys [text]}]
           {:keys [entity/mouseover?] :as entity*}
           g
           _ctx]
    (when (and mouseover? text)
      (let [[x y] (:position entity*)]
        (draw-text g
                   {:text text
                    :x x
                    :y (+ y (:half-height entity*))
                    :up? true})))))