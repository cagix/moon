(ns cdq.ui.error-window
  (:require [cdq.utils :refer [with-err-str]]
            [gdl.ui :as ui]))

(defn create [throwable]
  (ui/window {:title "Error"
              :rows [[(ui/label (binding [*print-level* 3]
                                  (with-err-str
                                    (clojure.repl/pst throwable))))]]
              :modal? true
              :close-button? true
              :close-on-escape? true
              :center? true
              :pack? true}))
