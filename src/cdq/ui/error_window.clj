(ns cdq.ui.error-window
  (:require [cdq.ui :as ui]
            [clojure.repl]
            [clojure.scene2d :as scene2d]
            [clojure.utils :as utils]
            [clojure.vis-ui.label :as label]))

(extend-type cdq.ui.Stage
  ui/ErrorWindow
  (show-error-window! [stage throwable]
    (.addActor stage (scene2d/build
                      {:actor/type :actor.type/window
                       :title "Error"
                       :rows [[{:actor (label/create (binding [*print-level* 3]
                                                       (utils/with-err-str
                                                         (clojure.repl/pst throwable))))}]]
                       :modal? true
                       :close-button? true
                       :close-on-escape? true
                       :center? true
                       :pack? true}))))
