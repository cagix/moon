(ns init.files)

(defn do! [{:keys [init/application]
            :as init}]
  (set! (.files application) (.createFiles application))
  init)
