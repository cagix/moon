(ns cdq.error
  (:require [cdq.scene2d.stage :as stage]
            [cdq.ui :as ui]
            [cdq.utils :refer [pretty-pst with-err-str]]))

(defn error-window [{:keys [cdq.context/stage]} throwable]
  (pretty-pst throwable)
  (stage/add-actor stage
                   (ui/window {:title "Error"
                               :rows [[(ui/label (binding [*print-level* 3]
                                                   (with-err-str
                                                     (clojure.repl/pst throwable))))]]
                               :modal? true
                               :close-button? true
                               :close-on-escape? true
                               :center? true
                               :pack? true})))
