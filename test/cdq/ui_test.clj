(ns cdq.ui-test
  (:require [cdq.application :as application]
            #_[cdq.create.world-event-handlers :refer [show-modal-window!]]
            [cdq.ui.widget :as widget]
            [gdl.scene2d.stage :as stage]))

(comment
 ; TODO use post-runnable! ?

 (show-modal-window! @application/state
                     {:title "Hello Modal"
                      :text "MY TEXT"
                      :button-text "MYBUTONTEXT"
                      :on-click (fn []
                                  (println "CLIEKDE"))})

 (stage/add! (:ctx/stage @application/state)
             (widget/error-window (ex-info "MY MESSAGE"
                                           {:m1 :k1
                                            :m2 :k2}
                                           (Throwable. "foobarcause"))))
 )
