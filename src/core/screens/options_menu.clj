(ns ^:no-doc core.screens.options-menu
  (:require [core.utils.core :refer [safe-get]]
            [core.utils.ns :as ns]
            [core.ui :as ui]
            [core.ctx :refer :all]
            [core.ctx.screens :as screens]
            [core.screens.stage :as stage]
            [core.screen :as screen]
            [core.widgets.background-image :refer [->background-image]])
  (:import com.badlogic.gdx.Input$Keys))

(defprotocol StatusCheckBox
  (get-text [this])
  (get-state [this])
  (set-state [this is-selected]))

(deftype VarStatusCheckBox [^clojure.lang.Var avar]
  StatusCheckBox
  (get-text [this]
    (let [m (meta avar)]
      (str "[LIGHT_GRAY]" (str (:ns m)) "/[WHITE]" (name (:name m)) "[]")))

  (get-state [this]
    @avar)

  (set-state [this is-selected]
    (.bindRoot avar is-selected)))

(defn- debug-flags [] ;
  (apply concat
         ; TODO
         (for [nmspace (ns/get-namespaces #{"core"})] ; DRY in core.component check ns-name & core.app require all ... core.components
           (ns/get-vars nmspace (fn [avar] (:dbg-flag (meta avar)))))))

; TODO FIXME IF THE FLAGS ARE CHANGED MANUALLY IN THE REPL THIS IS NOT REFRESHED
; -. rebuild it on window open ...
(def ^:private debug-flags (map ->VarStatusCheckBox (debug-flags)))

(def ^:private key-help-text
  "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[TAB] - Minimap\n[P]/[SPACE] - Unpause")

(defn- create-table [{:keys [context/config] :as ctx}]
  (ui/->table {:rows (concat
                      [[(ui/->label key-help-text)]]

                      (when (safe-get config :debug-window?)
                        [[(ui/->label "[Z] - Debug window")]])

                      (when (safe-get config :debug-options?)
                        (for [check-box debug-flags]
                          [(ui/->check-box (get-text check-box)
                                           (partial set-state check-box)
                                           (boolean (get-state check-box)))]))

                      [[(ui/->text-button "Resume" #(screens/change-screen % :screens/world))]

                       [(ui/->text-button "Exit" #(screens/change-screen % :screens/main-menu))]])

               :fill-parent? true
               :cell-defaults {:pad-bottom 10}}))

(defcomponent ::sub-screen
  (screen/render [_ ctx]
    (if (.isKeyJustPressed gdx-input Input$Keys/ESCAPE)
      (screens/change-screen ctx :screens/world)
      ctx)))

(derive :screens/options-menu :screens/stage)
(defcomponent :screens/options-menu
  (->mk [_ ctx]
    {:stage (stage/create ctx
                          [(->background-image ctx)
                           (create-table ctx)])
     :sub-screen [::sub-screen]}))
