import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import VChart from 'vue-echarts';
import App from './App.vue';
import router from './router';
import './styles/variables.css';
import './styles/element-plus-override.css';
import './styles/base.css';
import './plugins/echarts';

const app = createApp(App);
app.use(createPinia());
app.use(router);
app.use(ElementPlus);
app.component('VChart', VChart);
app.mount('#app');
