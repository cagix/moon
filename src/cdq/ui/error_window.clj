(ns cdq.ui.error-window
  (:require [gdl.ui :as ui]
            [gdl.utils :refer [with-err-str]]))

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
