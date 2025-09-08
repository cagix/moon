(ns cdq.editor-window
  (:require [cdq.db :as db]
            [cdq.property :as property]
            [cdq.stacktrace :as stacktrace]
            [cdq.editor.widget :as editor-widget]
            [cdq.ui.widget]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.ui.window :as window]
            [clojure.vis-ui.widget :as widget]))

(defn property-editor-window
  [{:keys [ctx/application-state
           ctx/db
           ctx/ui-viewport]
    :as ctx}
   property]
  (let [schema (get (:schemas db) (property/type property))
        widget (editor-widget/create schema nil property ctx)
        with-window-close (fn [f]
                            (fn [actor {:keys [ctx/stage] :as ctx}]
                              (try (f ctx)
                                   (actor/remove! (window/find-ancestor actor))
                                   (catch Throwable t
                                     (stacktrace/pretty-print t)
                                     (stage/add! stage (cdq.ui.widget/error-window t))))))
        clicked-save-fn (with-window-close (fn [{:keys [ctx/db]}]
                                             (swap! application-state update :ctx/db
                                                    db/update!
                                                    (editor-widget/value schema nil widget (:schemas db)))))
        clicked-delete-fn (with-window-close (fn [_ctx]
                                               (swap! application-state update :ctx/db
                                                      db/delete!
                                                      (:property/id property))))
        extra-act-fn (fn [actor _delta {:keys [ctx/input] :as ctx}]
                       (when (input/key-just-pressed? input :enter)
                         (clicked-save-fn actor ctx)))
        scrollpane-height (:viewport/height ui-viewport)]
    (doto (widget/window {:title (str "[SKY]Property[]")
                          :id :property-editor-window
                          :modal? true
                          :close-button? true
                          :center? true
                          :close-on-escape? true
                          :rows [[(cdq.ui.widget/scroll-pane-cell scrollpane-height
                                                                  [[{:actor widget :colspan 2}]
                                                                   [{:actor (widget/text-button "Save [LIGHT_GRAY](ENTER)[]" clicked-save-fn)
                                                                     :center? true}
                                                                    {:actor (widget/text-button "Delete" clicked-delete-fn)
                                                                     :center? true}]])]]
                          :actors [{:actor/type :actor.type/actor
                                    :act extra-act-fn}]
                          :cell-defaults {:pad 5}})
      (.pack))))
