(ns cdq.error
  (:require [clojure.error :refer [pretty-pst]]
            [clojure.scene2d.stage :as stage]
            [clojure.ui :as ui]
            [clojure.utils :refer [with-err-str]]))

(defn error-window [{:keys [clojure.context/stage]} throwable]
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
