import dayjs from 'dayjs';

export const formatDate = (value?: string | Date | null) => {
  if (!value) return '-';
  return dayjs(value).format('YYYY-MM-DD HH:mm');
};

export const normalizeIntervals = (value: string) => value
  .split(',')
  .map((item) => Number(item.trim()))
  .filter((item) => !Number.isNaN(item) && item > 0);

export const downloadJson = (filename: string, payload: unknown) => {
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
};
