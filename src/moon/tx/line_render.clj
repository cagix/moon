(ns moon.tx.line-render
  (:require [moon.body :as body]))

(defn handle [{:keys [start end duration color thick?]}]
  [[:e/create
    start
    body/effect-body-props
    #:entity {:line-render {:thick? thick? :end end :color color}
              :delete-after-duration duration}]])
