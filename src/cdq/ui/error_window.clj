(ns cdq.ui.error-window
  (:require [cdq.ui :as ui]
            [clojure.repl]
            [gdl.scene2d :as scene2d]
            [gdl.scene2d.stage :as stage]
            [gdl.utils :as utils]))

(extend-type com.badlogic.gdx.scenes.scene2d.CtxStage
  ui/ErrorWindow
  (show-error-window! [stage throwable]
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
                        :pack? true}))))
