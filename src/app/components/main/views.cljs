(ns app.components.main.views
  (:require
   [app.state.context :as state.context]
   [uix.core :as uix :refer [$ defui]]
   [app.components.editor.views :refer [Editor]]))

(defui provider []
  ($ state.context/provider {:middleware state.context/middleware}
     ($ Editor)))
