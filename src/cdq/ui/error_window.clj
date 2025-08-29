(ns cdq.ui.error-window
  (:require [cdq.utils :refer [with-err-str]]
            [gdx.ui :as ui]
            clojure.repl))

(defn create [throwable]
  (ui/window {:title "Error"
              :rows [[{:actor {:actor/type :actor.type/label
                               :label/text (binding [*print-level* 3]
                                             (with-err-str
                                               (clojure.repl/pst throwable)))}}]]
              :modal? true
              :close-button? true
              :close-on-escape? true
              :center? true
              :pack? true}))
