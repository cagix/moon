(ns moon.tx.line-render
  (:require [moon.component :as component]
            [moon.entity :as entity]))

(defc :tx/line-render
  (component/handle [[_ {:keys [start end duration color thick?]}]]
    [[:e/create
      start
      entity/effect-body-props
      #:entity {:line-render {:thick? thick? :end end :color color}
                :delete-after-duration duration}]]))
