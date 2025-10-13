(ns cdq.ui.error-window
  (:require [cdq.ui :as ui]
            [cdq.ui.stage :as stage]
            [clojure.repl]
            [clojure.scene2d.vis-ui.window :as window]
            [clojure.utils :as utils]
            [clojure.vis-ui.label :as label]))

(extend-type cdq.ui.Stage
  ui/ErrorWindow
  (show-error-window! [stage throwable]
    (stage/add-actor! stage
                      (window/create
                       {:title "Error"
                        :rows [[{:actor (label/create (binding [*print-level* 3]
                                                        (utils/with-err-str
                                                          (clojure.repl/pst throwable))))}]]
                        :modal? true
                        :close-button? true
                        :close-on-escape? true
                        :center? true
                        :pack? true}))))
