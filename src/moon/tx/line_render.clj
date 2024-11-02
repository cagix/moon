(ns moon.tx.line-render
  (:require [moon.component :as component]
            [moon.body :as body]))

(defmethods :tx/line-render
  (component/handle [[_ {:keys [start end duration color thick?]}]]
    [[:e/create
      start
      body/effect-body-props
      #:entity {:line-render {:thick? thick? :end end :color color}
                :delete-after-duration duration}]]))
