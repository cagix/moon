(ns cdq.ui.error-window
  (:require [cdq.ui :as ui]
            [clojure.repl]
            [clojure.scene2d :as scene2d]
            [cdq.ui.stage :as stage]
            [clojure.utils :as utils]))

(extend-type cdq.ui.Stage
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
