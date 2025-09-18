(ns cdq.tx.show-error-window
  (:require [cdq.string :as string]
            [gdl.scene2d :as scene2d]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage]
            [clojure.repl]))

(defn do! [{:keys [ctx/stage]} throwable]
  (stage/add! stage (scene2d/build
                     {:actor/type :actor.type/window
                      :title "Error"
                      :rows [[{:actor {:actor/type :actor.type/label
                                       :label/text (binding [*print-level* 3]
                                                     (string/with-err-str
                                                       (clojure.repl/pst throwable)))}}]]
                      :modal? true
                      :close-button? true
                      :close-on-escape? true
                      :center? true
                      :pack? true})))
