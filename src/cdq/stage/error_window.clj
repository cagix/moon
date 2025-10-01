(ns cdq.stage.error-window
  (:require [clojure.repl]
            [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]
            [gdl.utils :as utils]))

(defn show! [stage throwable]
  (stage/add! stage (scene2d/build
                     {:actor/type :actor.type/window
                      :title "Error"
                      :rows [[{:actor {:actor/type :actor.type/label
                                       :label/text (binding [*print-level* 3]
                                                     (utils/with-err-str
                                                       (clojure.repl/pst throwable)))}}]]
                      :modal? true
                      :close-button? true
                      :close-on-escape? true
                      :center? true
                      :pack? true})))
