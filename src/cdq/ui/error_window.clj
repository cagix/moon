(ns cdq.ui.error-window
  (:require [clojure.gdx.scene2d.ui :as ui]
            [clojure.utils :as utils]))

(defn create [throwable]
  (ui/window {:title "Error"
              :rows [[(ui/label (binding [*print-level* 3]
                                  (utils/with-err-str
                                    (clojure.repl/pst throwable))))]]
              :modal? true
              :close-button? true
              :close-on-escape? true
              :center? true
              :pack? true}))
