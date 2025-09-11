(ns cdq.editor.window
  (:require [cdq.ctx :as ctx]
            [cdq.ui.widget]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.ui.window :as window]))

(defn create
  [{:keys [save-fn
           delete-fn
           scrollpane-height
           widget]}]
  (let [with-window-close (fn [f]
                            (fn [actor {:keys [ctx/stage] :as ctx}]
                              (try (f ctx)
                                   (actor/remove! (window/find-ancestor actor))
                                   (catch Throwable t
                                     (ctx/handle-txs! ctx [[:tx/print-stacktrace  t]
                                                           [:tx/show-error-window t]])))))
        clicked-save-fn   (with-window-close save-fn)
        clicked-delete-fn (with-window-close delete-fn)]
    {:actor/type :actor.type/window
     :title "[SKY]Property[]"
     :id :property-editor-window
     :modal? true
     :close-button? true
     :center? true
     :close-on-escape? true
     :rows [[(cdq.ui.widget/scroll-pane-cell scrollpane-height
                                             [[{:actor widget :colspan 2}]
                                              [{:actor {:actor/type :actor.type/text-button
                                                        :text "Save [LIGHT_GRAY](ENTER)[]"
                                                        :on-clicked clicked-save-fn}
                                                :center? true}
                                               {:actor {:actor/type :actor.type/text-button
                                                        :text "Delete"
                                                        :on-clicked clicked-delete-fn}
                                                :center? true}]])]]
     :actors [{:actor/type :actor.type/actor
               :act (fn [actor _delta {:keys [ctx/input] :as ctx}]
                      (when (input/key-just-pressed? input :enter)
                        (clicked-save-fn actor ctx)))}]
     :cell-defaults {:pad 5}
     :pack? true}))
