(ns init.add-window)

(defn do! [{:keys [init/window
                   init/application]
            :as init}]
  (.add (.windows application) window)
  init)
