(ns cdq.graphics)

; dont pass vector & key
; these are transactions on an object
; is there something like this
; just function calls
; ctx first param
(defn handle-draws!
  [{:keys [ctx/draw-fns]
    :as ctx}
   draws]
  (doseq [{k 0 :as component} draws
          :when component]
    (apply (draw-fns k) ctx (rest component))))
