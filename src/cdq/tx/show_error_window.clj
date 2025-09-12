(ns cdq.tx.show-error-window
  (:require [cdq.string :as string]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.stage :as stage]
            [clojure.vis-ui.widget :as widget]
            [clojure.repl]))

(defn do! [{:keys [ctx/stage]} throwable]
  (stage/add! stage (widget/window
                     {:title "Error"
                      :rows [[{:actor {:actor/type :actor.type/label
                                       :label/text (binding [*print-level* 3]
                                                     (string/with-err-str
                                                       (clojure.repl/pst throwable)))}}]]
                      :modal? true
                      :close-button? true
                      :close-on-escape? true
                      :center? true
                      :pack? true})))
