import { render, screen } from '@testing-library/react';
import React from 'react';

jest.mock('react-router-dom', () => ({
  BrowserRouter: ({ children }) => <div>{children}</div>,
  Routes: ({ children }) => <div>{children}</div>,
  Route: ({ element }) => <div>{element}</div>,
  Navigate: () => <div>Navigate</div>,
  Link: ({ to, children }) => <a href={to}>{children}</a>,
  useNavigate: () => jest.fn(),
  useLocation: () => ({ pathname: '/' }),
}));

import App from './App';

test('renders CarePortal main application without crashing', () => {
  render(<App />);
  const brandElements = screen.getAllByText(/CarePortal/i);
  expect(brandElements.length).toBeGreaterThan(0);
});


