(ns cdq.val-max
  (:require [cdq.malli :as m]))

; What is the use case of this ?
; How is it used?
; Maybe I can find another way
; stats ops pre/post conditions ? after-effects ?
; ask chatpt?
(def schema
  (m/schema [:and
             [:vector {:min 2 :max 2} [:int {:min 0}]]
             [:fn {:error/fn (fn [{[^int v ^int mx] :value} _]
                               (when (< mx v)
                                 (format "Expected max (%d) to be smaller than val (%d)" v mx)))}
              (fn [[^int a ^int b]] (<= a b))]]))

(defn ratio
  "If mx and v is 0, returns 0, otherwise (/ v mx)"
  [[^int v ^int mx]]
  {:pre [(m/validate schema [v mx])]}
  (if (and (zero? v) (zero? mx))
    0
    (/ v mx)))
